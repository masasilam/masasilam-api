package com.naskah.demo.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
public class EbookPojo {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String id;
    @NotBlank(message = "Judul tidak boleh kosong.")
    @Size(max = 255)
    private String title;
    @NotBlank(message = "Penulis tidak boleh kosong.")
    private String author;
    @NotNull(message = "Tahun tidak boleh kosong.")
    private int year;
    @NotBlank(message = "File path tidak boleh kosong.")
    private String filePath;
}

