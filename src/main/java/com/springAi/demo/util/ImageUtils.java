package com.springAi.demo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

public class ImageUtils {
    public static class ImageData {
        public final Resource resource;
        public final String mimeType;
        public final String filename;

        public ImageData(Resource resource, String mimeType, String filename) {
            this.resource = resource;
            this.mimeType = mimeType;
            this.filename = filename;
        }
    }

    public static ImageData resourceFromUrl(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl is required");
        }

        // data URL
        if (imageUrl.startsWith("data:")) {
            int comma = imageUrl.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException("Invalid data URL");
            String meta = imageUrl.substring(5, comma);
            boolean isBase64 = meta.endsWith(";base64");
            String mime = isBase64 ? meta.substring(0, meta.length() - ";base64".length()) : meta;
            if (mime.isBlank()) mime = MimeTypeUtils.IMAGE_JPEG_VALUE;
            if (!isBase64) throw new IllegalArgumentException("Only base64 data URLs are supported");
            byte[] bytes = Base64.getDecoder().decode(imageUrl.substring(comma + 1).trim());
            String ext = mimeToExtension(mime);
            String filename = "image" + (ext.isBlank() ? ".jpg" : "." + ext);
            Resource res = new NamedByteArrayResource(bytes, filename);
            return new ImageData(res, mime, filename);
        }

        // file path or file://
        if (imageUrl.startsWith("file:") || (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://"))) {
            File f = imageUrl.startsWith("file:") ? new File(new URI(imageUrl).toURL().toURI()) : new File(imageUrl);
            if (!f.exists()) throw new IllegalArgumentException("File not found: " + imageUrl);
            byte[] bytes = Files.readAllBytes(f.toPath());
            String mime = Files.probeContentType(f.toPath());
            if (mime == null || mime.isBlank()) mime = MimeTypeUtils.IMAGE_JPEG_VALUE;
            Resource res = new NamedByteArrayResource(bytes, f.getName());
            return new ImageData(res, mime, f.getName());
        }

        // http(s)
        URI uri = new URI(imageUrl);
        URL url = uri.toURL();
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        String contentType = conn.getContentType();
        if (contentType == null || contentType.isBlank()) contentType = MimeTypeUtils.IMAGE_JPEG_VALUE;
        String path = url.getPath();
        String filename = (path != null && !path.isBlank()) ? path.substring(path.lastIndexOf('/') + 1) : "image";
        if (!filename.contains(".")) {
            String ext = mimeToExtension(contentType);
            filename = filename + (ext.isBlank() ? ".jpg" : "." + ext);
        }

        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                bout.write(buf, 0, r);
            }
            byte[] bytes = bout.toByteArray();
            Resource res = new NamedByteArrayResource(bytes, filename);
            return new ImageData(res, contentType, filename);
        }
    }

    private static String mimeToExtension(String mime) {
        if (mime == null) return "";
        String m = mime.toLowerCase();
        if (m.contains("png")) return "png";
        if (m.contains("jpeg") || m.contains("jpg")) return "jpg";
        if (m.contains("gif")) return "gif";
        if (m.contains("webp")) return "webp";
        if (m.contains("bmp")) return "bmp";
        return "";
    }

    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
