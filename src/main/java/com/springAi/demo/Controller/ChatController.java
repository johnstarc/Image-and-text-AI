package com.springAi.demo.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import com.springAi.demo.dto.ImageChatRequest;
import com.springAi.demo.dto.TextChatRequest;
import com.springAi.demo.util.ImageUtils;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/text")
    public ResponseEntity<String> textChat(@RequestBody TextChatRequest request) {
        String reply = this.chatClient.prompt()
              .user(request.question())
              .call()
              .content();
        return ResponseEntity.ok(reply);
    }

    @PostMapping("/image")
    public ResponseEntity<String> imageChat(@RequestBody ImageChatRequest request) {
        try {
            String imageUrl = request.imageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                return ResponseEntity.badRequest().body("Image URL is required");
            }

            ImageUtils.ImageData imageData = ImageUtils.resourceFromUrl(imageUrl);

            MimeType mimeType;
            try {
                mimeType = MimeTypeUtils.parseMimeType(imageData.mimeType);
            } catch (Exception e) {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            }

            final MimeType finalMimeType = mimeType;
            final Resource resource = imageData.resource;

            String reply = this.chatClient.prompt()
                  .user(user -> user
                        .text(request.question())
                        .media(finalMimeType, resource)
                  )
                  .call()
                  .content();

            return ResponseEntity.ok(reply);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image input: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process image: " + e.getMessage());
        }
    }

}
