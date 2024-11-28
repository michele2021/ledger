package com.accounting.core.entities.views;

import com.accounting.commons.Constants;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JournalLineView {

    private long journalLineId;
    private Constants.RegisterType registerType;
    private long quantity;

}