package com.ieltspath.library.api.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateDeckRequest {
    @NotBlank
    @Size(max = 150)
    private String name;
    @Size(max = 2000)
    private String description;
    @NotNull
    private LibraryStatus status;
    @Size(max = 300)
    private String coverImageKey;

    private boolean coverImageKeySpecified = false;

    public UpdateDeckRequest(String name, String description, LibraryStatus status, String coverImageKey) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.coverImageKey = coverImageKey;
        this.coverImageKeySpecified = true;
    }

    public UpdateDeckRequest(String name, String description, LibraryStatus status) {
        this(name, description, status, null);
        this.coverImageKeySpecified = false;
    }

    @JsonSetter(value = "coverImageKey", nulls = Nulls.SET)
    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
        this.coverImageKeySpecified = true;
    }

    public boolean isCoverImageKeySpecified() {
        return coverImageKeySpecified;
    }

    public String name() { return name; }
    public String description() { return description; }
    public LibraryStatus status() { return status; }
    public String coverImageKey() { return coverImageKey; }
}
