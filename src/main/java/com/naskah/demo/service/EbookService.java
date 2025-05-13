package com.naskah.demo.service;

import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.DataResponse;
import com.naskah.demo.model.response.DatatableResponse;
import com.naskah.demo.model.response.DefaultResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

public interface EbookService {
    DataResponse<EbookPojo> create(EbookPojo ebookPojo, MultipartFile file) throws IOException;
    DataResponse<EbookPojo> findOne(String id);
    DatatableResponse<EbookPojo> getDatatable(int page, int limit, String sortField, String sortOrder);
    DataResponse<EbookPojo> update(String id, EbookPojo ebookPojo, MultipartFile file) throws IOException;
    DefaultResponse delete(String id) throws IOException;
    Resource downloadEbook(String id) throws MalformedURLException;
    Resource readEbook(String id) throws MalformedURLException;
}
