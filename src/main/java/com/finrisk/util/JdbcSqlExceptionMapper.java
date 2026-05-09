package com.finrisk.util;

import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.exception.InsufficientBalanceException;
import com.finrisk.exception.InsufficientQuantityException;

import java.sql.SQLException;

public final class JdbcSqlExceptionMapper {

    private JdbcSqlExceptionMapper() {}

    public static RuntimeException map(SQLException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        // RAISERROR strings from dbo.sp_buy_asset / dbo.sp_sell_asset
        if (msg.contains("INSUFFICIENT_BALANCE")) {
            return new InsufficientBalanceException("Insufficient cash balance");
        }
        if (msg.contains("INSUFFICIENT_QUANTITY")) {
            return new InsufficientQuantityException("Insufficient asset quantity");
        }
        if (msg.contains("ACCOUNT_NOT_FOUND")) {
            return new AccountNotFoundException("Account not found");
        }
        if (msg.contains("ASSET_NOT_FOUND")) {
            return new AssetNotFoundException("Asset not found");
        }
        return null;
    }
}
