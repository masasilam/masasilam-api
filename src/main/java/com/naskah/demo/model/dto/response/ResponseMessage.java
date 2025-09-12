package com.naskah.demo.model.dto.response;

import lombok.Data;

/**
 * A utility class containing standardized response messages for API responses.
 * Messages are provided in both English and Indonesian for localization support.
 */
@Data
public final class ResponseMessage {
    private ResponseMessage() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== SUCCESS MESSAGES ====================
    public static final String SUCCESS = "Success";
    public static final String BERHASIL = "Berhasil";

    public static final String DATA_CREATED = "Data successfully created.";
    public static final String DATA_DIBUAT = "Data berhasil dibuat.";

    public static final String DATA_UPDATED = "Data successfully updated.";
    public static final String DATA_DIPERBARUI = "Data berhasil diperbarui.";

    public static final String DATA_FETCHED = "Data successfully retrieved.";
    public static final String DATA_DIAMBIL = "Data berhasil diambil.";

    public static final String DATA_DELETED = "Data successfully deleted.";
    public static final String DATA_DIHAPUS = "Data berhasil dihapus.";

    public static final String OPERATION_SUCCESS = "Operation completed successfully.";
    public static final String OPERASI_BERHASIL = "Operasi berhasil diselesaikan.";

    public static final String LOGIN_SUCCESS = "Login successful.";
    public static final String LOGIN_BERHASIL = "Login berhasil.";

    public static final String LOGOUT_SUCCESS = "Logout successful.";
    public static final String LOGOUT_BERHASIL = "Logout berhasil.";

    public static final String REGISTRATION_SUCCESS = "Registration successful.";
    public static final String REGISTRASI_BERHASIL = "Registrasi berhasil.";

    // ==================== ERROR MESSAGES ====================
    public static final String ERROR = "Error";
    public static final String GALAT = "Galat";

    public static final String DATA_NOT_FOUND = "Requested data not found.";
    public static final String DATA_TIDAK_DITEMUKAN = "Data yang diminta tidak ditemukan.";

    public static final String NULL_DATA = "Null data is not allowed.";
    public static final String DATA_NULL = "Data null tidak diperbolehkan.";

    public static final String OUT_OF_BOUNDS = "Invalid array index accessed.";
    public static final String INDEKS_TIDAK_VALID = "Indeks array tidak valid diakses.";

    public static final String DATA_ALREADY_EXISTS = "Data already exists.";
    public static final String DATA_SUDAH_ADA = "Data sudah ada.";

    public static final String MISSING_PARAMETER = "Required parameter(s) are missing.";
    public static final String PARAMETER_HILANG = "Parameter yang diperlukan tidak ada.";

    public static final String INVALID_DATA = "Invalid data provided.";
    public static final String DATA_TIDAK_VALID = "Data yang diberikan tidak valid.";

    public static final String UNAUTHORIZED = "Unauthorized access.";
    public static final String TIDAK_TEROTORISASI = "Akses tidak diotorisasi.";

    public static final String FORBIDDEN = "Access forbidden.";
    public static final String DILARANG = "Akses dilarang.";

    public static final String VALIDATION_ERROR = "Validation failed.";
    public static final String VALIDASI_GAGAL = "Validasi gagal.";

    public static final String INTERNAL_SERVER_ERROR = "Internal server error.";
    public static final String KESALAHAN_SERVER = "Terjadi kesalahan pada server.";

    public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable.";
    public static final String LAYANAN_TIDAK_TERSEDIA = "Layanan sementara tidak tersedia.";

    public static final String BAD_REQUEST = "Invalid request format.";
    public static final String PERMINTAAN_TIDAK_VALID = "Format permintaan tidak valid.";

    public static final String METHOD_NOT_ALLOWED = "HTTP method not allowed for this endpoint.";
    public static final String METODE_TIDAK_DIIZINKAN = "Metode HTTP tidak diizinkan untuk endpoint ini.";

    // ==================== AUTHENTICATION MESSAGES ====================
    public static final String INVALID_CREDENTIALS = "Invalid username or password.";
    public static final String KREDENSIAL_TIDAK_VALID = "Username atau password tidak valid.";

    public static final String ACCOUNT_LOCKED = "Account is temporarily locked.";
    public static final String AKUN_DIKUNCI = "Akun sementara dikunci.";

    public static final String TOKEN_EXPIRED = "Session token has expired.";
    public static final String TOKEN_KADALUARSA = "Token sesi telah kadaluarsa.";

    public static final String INVALID_TOKEN = "Invalid authentication token.";
    public static final String TOKEN_TIDAK_VALID = "Token otentikasi tidak valid.";

    public static final String INSUFFICIENT_PRIVILEGES = "Insufficient privileges for this operation.";
    public static final String HAK_AKSES_TIDAK_CUKUP = "Hak akses tidak cukup untuk operasi ini.";

    // ==================== BUSINESS LOGIC MESSAGES ====================
    public static final String LIMIT_EXCEEDED = "Request limit exceeded.";
    public static final String BATAS_TERLAMPAUI = "Batas permintaan terlampaui.";

    public static final String CONFLICT = "Conflict with existing data.";
    public static final String KONFLIK = "Konflik dengan data yang ada.";

    public static final String PRECONDITION_FAILED = "Precondition failed.";
    public static final String PRASYARAT_GAGAL = "Prasyarat gagal.";

    public static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported media type.";
    public static final String TIPE_MEDIA_TIDAK_DIDUKUNG = "Tipe media tidak didukung.";

    public static final String RATE_LIMIT_EXCEEDED = "Too many requests. Please try again later.";
    public static final String BATAS_PERMINTAAN = "Terlalu banyak permintaan. Silakan coba lagi nanti.";

    // ==================== FILE OPERATION MESSAGES ====================
    public static final String FILE_UPLOAD_SUCCESS = "File uploaded successfully.";
    public static final String FILE_TERUPLOAD = "File berhasil diunggah.";

    public static final String FILE_UPLOAD_FAILED = "File upload failed.";
    public static final String GAGAL_UPLOAD_FILE = "Gagal mengunggah file.";

    public static final String INVALID_FILE_TYPE = "Invalid file type.";
    public static final String TIPE_FILE_TIDAK_VALID = "Tipe file tidak valid.";

    public static final String FILE_SIZE_EXCEEDED = "File size exceeds the allowed limit.";
    public static final String UKURAN_FILE_TERLALU_BESAR = "Ukuran file melebihi batas yang diizinkan.";

    public static final String FILE_NOT_FOUND = "Requested file not found.";
    public static final String FILE_TIDAK_DITEMUKAN = "File yang diminta tidak ditemukan.";

    // ==================== DATABASE OPERATION MESSAGES ====================
    public static final String DB_CONNECTION_ERROR = "Database connection error.";
    public static final String KONEKSI_DB_GAGAL = "Gagal terhubung ke database.";

    public static final String DB_QUERY_ERROR = "Database query error.";
    public static final String QUERY_DB_GAGAL = "Kesalahan query database.";

    public static final String DB_TRANSACTION_ERROR = "Database transaction error.";
    public static final String TRANSAKSI_DB_GAGAL = "Kesalahan transaksi database.";

    public static final String DB_CONSTRAINT_VIOLATION = "Database constraint violation.";
    public static final String PELANGGARAN_DB = "Pelanggaran constraint database.";

    // ==================== VALIDATION MESSAGES ====================
    public static final String INVALID_EMAIL = "Invalid email format.";
    public static final String EMAIL_TIDAK_VALID = "Format email tidak valid.";

    public static final String INVALID_PHONE = "Invalid phone number format.";
    public static final String NOMOR_HP_TIDAK_VALID = "Format nomor handphone tidak valid.";

    public static final String PASSWORD_REQUIREMENTS = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number and one special character.";
    public static final String SYARAT_PASSWORD = "Password harus terdiri dari minimal 8 karakter, mengandung minimal satu huruf besar, satu huruf kecil, satu angka dan satu karakter khusus.";

    public static final String REQUIRED_FIELD = "This field is required.";
    public static final String FIELD_DIBUTUHKAN = "Field ini wajib diisi.";

    public static final String INVALID_DATE_FORMAT = "Invalid date format. Expected format: yyyy-MM-dd.";
    public static final String FORMAT_TANGGAL_TIDAK_VALID = "Format tanggal tidak valid. Format yang diharapkan: yyyy-MM-dd.";
}