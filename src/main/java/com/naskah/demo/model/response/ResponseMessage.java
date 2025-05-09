package com.naskah.demo.model.response;

import lombok.Getter;

@Getter
public final class ResponseMessage {
    private ResponseMessage() {
        throw new UnsupportedOperationException("This is a constant class and cannot be instantiated");
    }
    public static final String DATA_CREATED = "Data successfully created.";
    public static final String DATA_UPDATED = "Data successfully modified.";
    public static final String DATA_FETCHED = "Data(s) successfully fetched.";
    public static final String DATA_DELETED = "Data successfully deleted.";
    public static final String DATA_NOT_FOUND = "Data not found.";
    public static final String NULL_DATA = "Null data is not allowed.";
    public static final String OUT_OF_BOUNDS = "Access an array of data at an invalid index.";
    public static final String DATA_ALREADY_EXISTS = "Data already exists.";
    public static final String MISSING_PARAMETER = "Some required parameter(s) are missing for this request.";
    public static final String DATA_INVALID = "Some of parameter(s) are invalid.";

}
