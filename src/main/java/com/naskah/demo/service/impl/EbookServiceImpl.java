package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.AlreadyExistsException;
import com.naskah.demo.exception.custom.NotFoundException;
import com.naskah.demo.mapper.EbookMapper;
import com.naskah.demo.model.pojo.EbookPojo;
import com.naskah.demo.model.response.*;
import com.naskah.demo.service.EbookService;
import com.naskah.demo.util.FileStorageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class EbookServiceImpl implements EbookService {
    private final EbookMapper ebookMapper;
    private final FileStorageUtil fileStorageUtil;
    private final String SUCCESS = "Success";
    private final Logger log = LogManager.getLogger(EbookServiceImpl.class);

    public EbookServiceImpl(EbookMapper ebookMapper, FileStorageUtil fileStorageUtil) {
        this.ebookMapper = ebookMapper;
        this.fileStorageUtil = fileStorageUtil;
    }

    @Override
    public DataResponse<EbookPojo> create(EbookPojo ebookPojo, MultipartFile file) throws IOException {
        try {
            int duplicateEbook = ebookMapper.countEbook(ebookPojo.getTitle(), ebookPojo.getAuthor(), ebookPojo.getYear());
            if (duplicateEbook > 0) {
                throw new AlreadyExistsException();
            }

            String id = UUID.randomUUID().toString();
            ebookPojo.setId(id);

            String filePath = fileStorageUtil.saveFile(file, id);
            ebookPojo.setFilePath(filePath);

            ebookMapper.insertEbook(ebookPojo);

            EbookPojo data = ebookMapper.getDetailEbook(id);
            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, HttpStatus.CREATED.value(), data);
        } catch (Exception e) {
            log.error("Error when insert ebook", e);
            throw e;
        }
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
    public DataResponse<EbookPojo> update(String id, EbookPojo ebookPojo, MultipartFile file) throws IOException {
        try {
            EbookPojo existingEbook = ebookMapper.getDetailEbook(id);
            if (existingEbook == null) {
                throw new NotFoundException();
            }

            ebookPojo.setId(id);

            if (file != null && !file.isEmpty()) {
                Path oldFilePath = Paths.get(existingEbook.getFilePath());
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                }

                String filePath = fileStorageUtil.saveFile(file, id);
                ebookPojo.setFilePath(filePath);
            } else {
                ebookPojo.setFilePath(existingEbook.getFilePath());
            }

            ebookMapper.updateEbook(ebookPojo);
            EbookPojo data = ebookMapper.getDetailEbook(id);
            if (data != null) {
                return new DataResponse<>(SUCCESS, ResponseMessage.DATA_UPDATED, HttpStatus.OK.value(), data);
            } else {
                throw new NotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when update ebook", e);
            throw e;
        }
    }

    @Override
    public DefaultResponse delete(String id) throws IOException {
        try {
            EbookPojo ebook = ebookMapper.getDetailEbook(id);
            if (ebook != null) {
                Path filePath = Paths.get(ebook.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                ebookMapper.deleteEbook(id);
                return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());
            } else {
                throw new NotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when delete ebook", e);
            throw e;
        }
    }

    @Override
    public Resource downloadEbook(String id) throws MalformedURLException {
        try {
            EbookPojo ebook = ebookMapper.getDetailEbook(id);
            if (ebook == null) {
                throw new NotFoundException();
            }

            Path filePath = Paths.get(ebook.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new NotFoundException();
            }
        } catch (Exception e) {
            log.error("Error when download ebook", e);
            throw e;
        }
    }

    @Override
    public Resource readEbook(String id) throws MalformedURLException {
        return downloadEbook(id);
    }
}
