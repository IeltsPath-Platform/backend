package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameCatalogResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetGameCatalogUseCase {
    public GameCatalogResult execute() {
        return new GameCatalogResult(List.of(
                new GameCatalogResult.Item("ROCKET", "Tên lửa",
                        "Chọn từ tiếng Anh phù hợp với nghĩa được cho.", "AVAILABLE"),
                new GameCatalogResult.Item("WORDLY", "Wordly",
                        "Đoán từ tiếng Anh dựa trên nghĩa và gợi ý từng chữ cái.", "AVAILABLE")
        ));
    }
}
