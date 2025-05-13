package com.naskah.demo.controller;

import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.DataResponse;
import com.naskah.demo.model.response.DatatableResponse;
import com.naskah.demo.model.response.DefaultResponse;
import com.naskah.demo.service.EbookService;
import com.naskah.demo.util.FileTypeUtil;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/ebooks")
public class EbookController {
    private final EbookService ebookService;
    private final FileTypeUtil fileTypeUtil;

    public EbookController(EbookService ebookService, FileTypeUtil fileTypeUtil) {
        this.ebookService = ebookService;
        this.fileTypeUtil = fileTypeUtil;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<EbookPojo>> create(
            @RequestPart("ebook") @Valid EbookPojo ebookPojo,
            @RequestPart("file") MultipartFile file) throws IOException {
        DataResponse<EbookPojo> response = ebookService.create(ebookPojo, file);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/find")
    public ResponseEntity<DataResponse<EbookPojo>> getDatatable( @RequestParam String id) {
        DataResponse<EbookPojo> data = ebookService.findOne(id);
        return ResponseEntity.ok().body(data);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<EbookPojo>> getDatatable(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "title", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<EbookPojo> list = ebookService.getDatatable(page, limit, sortField, sortOrder);
        return ResponseEntity.ok().body(list);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<EbookPojo>> update(
            @RequestParam String id,
            @RequestPart("ebook") @Valid EbookPojo ebookPojo,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        DataResponse<EbookPojo> response = ebookService.update(id, ebookPojo, file);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping
    public ResponseEntity<DefaultResponse> delete(@RequestParam String id) throws IOException {
        DefaultResponse response = ebookService.delete(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String id) throws MalformedURLException {
        Resource resource = ebookService.downloadEbook(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/read")
    public ResponseEntity<Resource> readEbook(@RequestParam String id) throws IOException {
        Resource resource = ebookService.readEbook(id);
        String contentType = fileTypeUtil.contentType(resource.getFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
