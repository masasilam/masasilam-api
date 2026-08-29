package com.masasilam.app.model.dto.monograph;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MonographStructureRequest {
    @NotEmpty
    @Valid
    private List<MonographChapterInput> chapters;
    @Valid
    private List<MonographEntryInput> counterpoints;
    @Valid
    private List<MonographEntryInput> interludes;
    @Valid
    private List<MonographEntryInput> peripheralDocuments;
    @Valid
    private List<MonographEntryInput> appendices;
    @Valid
    private List<MonographEntryInput> carryForwardPool;
    @Valid
    private List<MonographCriticalNoteInput> criticalNotes;
    @Valid
    private List<MonographGlossaryInput> glossary;
    @Valid
    private List<MonographIndexInput> nameIndex;
    @Valid
    private List<MonographIndexInput> placeIndex;
    @Valid
    private List<MonographIndexInput> orgIndex;
}