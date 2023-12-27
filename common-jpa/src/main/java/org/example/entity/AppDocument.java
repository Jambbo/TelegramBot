package org.example.entity;

import lombok.*;
import javax.persistence.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_document")
@Entity
public class AppDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String telegramField;
    private String docName;
    @OneToOne
    private BinaryContent binaryContent;
    private String mimeType;
    private Long fileSize;
}
