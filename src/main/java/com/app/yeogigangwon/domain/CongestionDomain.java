// backend/src/main/java/com/app/yeogigangwon/domain/CrowdRecord.java
package com.app.yeogigangwon.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crowd_records")
public class CongestionDomain {
    @Id
    private String id;
    private String beachId;
    private String beachName;
    private int personCount;
    private LocalDateTime timestamp;
}