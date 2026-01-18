package com.yann.forcesub.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "settings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppSetting {

    @Id
    private String key;

    private String value;
}
