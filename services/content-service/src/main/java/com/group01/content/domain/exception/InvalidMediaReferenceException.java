package com.group01.content.domain.exception;

import com.group01.content.domain.vo.AssetType;

/** A stored media reference is not one a client may load; a data defect, not a client error. */
public class InvalidMediaReferenceException extends ContentDomainException {
    public InvalidMediaReferenceException(AssetType assetType) {
        super("Invalid media reference for asset type " + assetType);
    }
}
