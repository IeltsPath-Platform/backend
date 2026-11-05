UPDATE topics
SET required_feature_key = NULL
WHERE required_feature_key IS NOT NULL;
