            try (PreparedStatement columns = connection.prepareStatement("""
                    SELECT column_name, data_type FROM information_schema.columns
                    WHERE table_name = 'notes' AND column_name IN ('source_type', 'source_reference_id')
                    ORDER BY column_name
                    """);
                 ResultSet result = columns.executeQuery()) {
                result.next();
                assertEquals("source_reference_id", result.getString(1));
                assertEquals("uuid", result.getString(2));
                result.next();
                assertEquals("source_type", result.getString(1));
                assertEquals("character varying", result.getString(2));
                assertEquals(false, result.next());
            }
            try (PreparedStatement constraint = connection.prepareStatement("""
                    SELECT COUNT(*) FROM pg_constraint
                    WHERE conname = 'chk_notes_source_pair' AND conrelid = 'notes'::regclass
                    """);
                 ResultSet result = constraint.executeQuery()) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
            try (PreparedStatement index = connection.prepareStatement("""
                    SELECT COUNT(*) FROM pg_indexes
                    WHERE tablename = 'notes' AND indexname = 'idx_notes_user_source'
                    """);
                 ResultSet result = index.executeQuery()) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }
        UUID userId = UUID.randomUUID();
        assertThrows(SQLException.class, () ->
                insertNote(userId, "TUTOR_SESSION", null));
        assertThrows(SQLException.class, () ->
                insertNote(userId, null, UUID.randomUUID()));
    }

    @Test
    void oldNotesRemainReadableAndSourceFilterIsUserScoped() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID oldId = insertNote(userId, null, null);
        UUID matchingId = insertNote(userId, "KNOWLEDGE_POINT", sourceId);
        insertNote(otherUserId, "KNOWLEDGE_POINT", sourceId);
        insertNote(userId, "TUTOR_SESSION", sourceId);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT source_type, source_reference_id FROM notes WHERE id = ?
                     """)) {
            statement.setObject(1, oldId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertNull(result.getString(1));
                assertNull(result.getObject(2));
            }
        }
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id FROM notes
                     WHERE user_id = ? AND status = 'ACTIVE'
                       AND source_type = ? AND source_reference_id = ?
                     ORDER BY updated_at DESC
                     """)) {
            statement.setObject(1, userId);
            statement.setString(2, "KNOWLEDGE_POINT");
            statement.setObject(3, sourceId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertEquals(matchingId, result.getObject(1));
                assertEquals(false, result.next());
            }
        }
    }

    private static UUID insertNote(UUID userId, String sourceType, UUID sourceReferenceId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO notes (id, user_id, title, body, status, created_at, updated_at,
                                        source_type, source_reference_id)
                     VALUES (?, ?, 'Note', 'Body', 'ACTIVE', now(), now(), ?, ?)
                     """)) {
            statement.setObject(1, id);
            statement.setObject(2, userId);
            statement.setString(3, sourceType);
            statement.setObject(4, sourceReferenceId);
            statement.executeUpdate();
        }
        return id;
    }

    private static void insertProgress(UUID userId, UUID videoId) throws SQLException {
        String sql = """
                INSERT INTO video_learning_progress (
                    id, user_id, video_id, last_position_ms, watched_duration_seconds,
                    progress_percent, status, updated_at
                ) VALUES (?, ?, ?, 0, 0, 0, 'NOT_STARTED', now())
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, videoId);
            statement.executeUpdate();
        }
    }

    private static void insertDeck(UUID userId, String name, String status) throws SQLException {
        String sql = """
                INSERT INTO flashcard_decks (id, user_id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, now(), now())
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setString(3, name);
            statement.setString(4, status);
            statement.executeUpdate();
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }