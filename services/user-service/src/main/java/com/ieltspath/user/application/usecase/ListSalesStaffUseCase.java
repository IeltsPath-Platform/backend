package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.SalesStaffReader;
import com.ieltspath.user.application.port.SalesStaffReader.SalesAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSalesStaffUseCase {
    private final SalesStaffReader reader;

    public List<SalesAccount> execute() {
        return reader.findAll();
    }
}
