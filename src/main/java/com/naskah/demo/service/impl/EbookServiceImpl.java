package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.NotFoundException;
import com.naskah.demo.mapper.EbookMapper;
import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.*;
import com.naskah.demo.service.EbookService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EbookServiceImpl implements EbookService {
    private final EbookMapper ebookMapper;
    private final String SUCCESS = "Success";
    private final Logger log = LogManager.getLogger(EbookServiceImpl.class);

    public EbookServiceImpl(EbookMapper ebookMapper) {
        this.ebookMapper = ebookMapper;
    }

    @Override
    public DataResponse<EbookPojo> findOne(String id) {
        try{
            EbookPojo data = ebookMapper.getDetailEbook(id);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
            } else {
                throw new NotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when get detail ebook", e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<EbookPojo> getDatatable(int page, int limit, String sortField, String sortOrder) {
        try {
            Map<String, String> allowedOrder = new HashMap<>();
            allowedOrder.put("title", "TITLE");
            String sortColumn = "TITLE";
            if (allowedOrder.containsKey(sortField)) {
                sortColumn = allowedOrder.getOrDefault(sortField, null);
            }
            String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
            int offset = (page - 1) * limit;
            List<EbookPojo> pageResult = ebookMapper.getListEbook(offset, limit, sortColumn, sortType);
            PageDataResponse<EbookPojo> data = new PageDataResponse<>(page, limit, pageResult.size(), pageResult);

            return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
        } catch (Exception e) {
            log.error("Error when get datatable ebook", e);
            throw e;
        }
    }

    @Override
    public DefaultResponse delete(String id) {
        try {
            ebookMapper.deleteEbook(id);
            return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());
        } catch (Exception e){
            log.error("Error when delete ebook", e);
            throw e;
        }
    }
}
