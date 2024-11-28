package com.accounting.core.services;

import com.accounting.core.db.BatchDAO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BatchService {


    private final BatchDAO batchDAO;

    public void executeMaintenanceTask(List<String> accountCodes) {
        //TODO will be implemented in LED-45
    }


}
