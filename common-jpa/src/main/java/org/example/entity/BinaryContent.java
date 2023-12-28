package org.example.entity;


import lombok.*;

import javax.persistence.*;
@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "binary_conent")
@Entity
public class BinaryContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private byte[] fileAsArrayOfBytes;
}
