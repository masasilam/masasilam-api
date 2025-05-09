package com.naskah.demo.controller;

import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.DataResponse;
import com.naskah.demo.model.response.DatatableResponse;
import com.naskah.demo.model.response.DefaultResponse;
import com.naskah.demo.service.EbookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;

@RestController
@RequestMapping("/ebooks")
public class EbookController {
    private final EbookService ebookService;

    public EbookController(EbookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping(path = "/find")
    public ResponseEntity<DataResponse<EbookPojo>> getDatatable( @RequestParam String id) {
        DataResponse<EbookPojo> data = ebookService.findOne(id);
        return ResponseEntity.ok().body(data);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<EbookPojo>> getDatatable(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "title", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder
    ) {
        DatatableResponse<EbookPojo> list = ebookService.getDatatable(page, limit, sortField, sortOrder);
        return ResponseEntity.ok().body(list);
    }

    @DeleteMapping
    public ResponseEntity<DefaultResponse> delete (@RequestParam String id) {
        DefaultResponse response = ebookService.delete(id);
        return ResponseEntity.ok().body(response);
    }
}
