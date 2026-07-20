package com.naskah.app.service.film.video;

/**
 * Strategy interface untuk video provider.
 *
 * Setiap provider (YouTube, Archive.org, Vimeo, dll) mengimplementasikan
 * interface ini. Spring Boot akan mengumpulkan semua implementasi
 * secara otomatis via List<VideoProvider> injection.
 *
 * Cara menambah provider baru:
 *   1. Buat class baru implements VideoProvider
 *   2. Beri @Component dan @Order(N)
 *   3. Selesai — tidak perlu ubah kode lain.
 */
public interface VideoProvider {

    /**
     * Cek apakah provider ini bisa menangani URL yang diberikan.
     *
     * @param url URL video mentah
     * @return true jika provider mendukung URL ini
     */
    boolean supports(String url);

    /**
     * Resolve URL menjadi metadata lengkap (embed URL, direct URL, thumbnail, dll).
     *
     * @param url URL video mentah
     * @return VideoMetadata hasil resolusi
     */
    VideoMetadata resolve(String url);

    /**
     * Tipe provider ini. Digunakan untuk logging dan penyimpanan di DB.
     */
    VideoProviderType getType();

    /**
     * Urutan prioritas pengecekan. Makin kecil = dicek lebih dulu.
     * Provider spesifik (YouTube=1) harus dicek sebelum fallback (DirectUrl=99).
     */
    int getOrder();
}