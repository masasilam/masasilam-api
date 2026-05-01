package com.naskah.demo.util.opds;

import com.naskah.demo.model.dto.opds.OpdsFeed;
import com.naskah.demo.model.dto.opds.OpdsEntry;
import com.naskah.demo.model.dto.opds.OpdsLink;
import com.naskah.demo.model.dto.opds.OpdsAuthor;
import org.springframework.stereotype.Component;

@Component
public class OpdsXmlBuilder {

    public String buildFeed(OpdsFeed feed) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\"\n");
        sb.append("      xmlns:opds=\"http://opds-spec.org/2010/catalog\"\n");
        sb.append("      xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\"\n");
        sb.append("      xmlns:dcterms=\"http://purl.org/dc/terms/\">\n");

        appendTag(sb, "id", feed.getId());
        appendTag(sb, "title", feed.getTitle());
        appendTag(sb, "updated", feed.getUpdated());

        // OpenSearch pagination (untuk feed buku)
        if (feed.getTotalResults() != null) {
            sb.append("  <opensearch:totalResults>")
                    .append(feed.getTotalResults())
                    .append("</opensearch:totalResults>\n");
            sb.append("  <opensearch:itemsPerPage>")
                    .append(feed.getItemsPerPage())
                    .append("</opensearch:itemsPerPage>\n");
            sb.append("  <opensearch:startIndex>")
                    .append(feed.getStartIndex())
                    .append("</opensearch:startIndex>\n");
        }

        // Links
        if (feed.getLinks() != null) {
            for (OpdsLink link : feed.getLinks()) {
                appendLink(sb, link);
            }
        }

        // Entries
        if (feed.getEntries() != null) {
            for (OpdsEntry entry : feed.getEntries()) {
                appendEntry(sb, entry);
            }
        }

        sb.append("</feed>");
        return sb.toString();
    }

    private void appendEntry(StringBuilder sb, OpdsEntry entry) {
        sb.append("  <entry>\n");
        appendTag(sb, "    ", "id", entry.getId());
        appendTag(sb, "    ", "title", escapeCdata(entry.getTitle()));
        appendTag(sb, "    ", "updated", entry.getUpdated());

        if (entry.getAuthors() != null) {
            for (OpdsAuthor author : entry.getAuthors()) {
                sb.append("    <author>\n");
                if (author.getName() != null) {
                    appendTag(sb, "      ", "name", author.getName());
                }
                if (author.getUri() != null) {
                    appendTag(sb, "      ", "uri", author.getUri());
                }
                sb.append("    </author>\n");
            }
        }

        if (entry.getSummary() != null) {
            sb.append("    <summary type=\"text\">")
                    .append(escape(entry.getSummary()))
                    .append("</summary>\n");
        }

        if (entry.getLinks() != null) {
            for (OpdsLink link : entry.getLinks()) {
                sb.append("  ");
                appendLink(sb, link);
            }
        }

        sb.append("  </entry>\n");
    }

    private void appendLink(StringBuilder sb, OpdsLink link) {
        sb.append("  <link");
        if (link.getRel() != null) sb.append(" rel=\"").append(link.getRel()).append("\"");
        if (link.getHref() != null) sb.append(" href=\"").append(link.getHref()).append("\"");
        if (link.getType() != null) sb.append(" type=\"").append(link.getType()).append("\"");
        if (link.getTitle() != null) sb.append(" title=\"").append(escape(link.getTitle())).append("\"");
        sb.append("/>\n");
    }

    private void appendTag(StringBuilder sb, String tag, String value) {
        appendTag(sb, "  ", tag, value);
    }

    private void appendTag(StringBuilder sb, String indent, String tag, String value) {
        if (value != null) {
            sb.append(indent).append("<").append(tag).append(">")
                    .append(escape(value))
                    .append("</").append(tag).append(">\n");
        }
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeCdata(String text) {
        return escape(text);
    }
}