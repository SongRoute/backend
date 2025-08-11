package com.app.yeogigangwon.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Congestion")

public class Congestion {
    @Id
    private String id;
    private String beachName;
    private int personCount;
    private String congestionLevel;
    private LocalDateTime timestamp;
}
