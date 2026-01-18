package com.yann.forcesub.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admins")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Admin {

    private Long id;
}
