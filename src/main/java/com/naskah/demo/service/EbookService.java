package com.naskah.demo.service;

import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.DataResponse;
import com.naskah.demo.model.response.DatatableResponse;
import com.naskah.demo.model.response.DefaultResponse;

public interface EbookService {
//    DataResponse<EbookPojo> create(EbookPojo barang);
    DataResponse<EbookPojo> findOne(String id);
//    DataResponse<EbookPojo> update(String idBarang, EbookPojo barang);
    DefaultResponse delete(String idBarang);
    DatatableResponse<EbookPojo> getDatatable(int page, int limit, String sortField, String sortOrder);
}
