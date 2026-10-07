package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.AssetType;

/** A stored media reference is not one a client may load; a data defect, not a client error. */
public class InvalidMediaReferenceException extends ContentDomainException {
    public InvalidMediaReferenceException(AssetType assetType) {
        super("Invalid media reference for asset type " + assetType);
    }
}
