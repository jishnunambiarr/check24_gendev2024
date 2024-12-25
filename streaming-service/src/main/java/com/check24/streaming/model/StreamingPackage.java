package com.check24.streaming.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class StreamingPackage 
{
    private final int streamingPackageId;
    private final String name;
    private final double monthlyPriceCents;
    private final double yearlyPriceCents;

    public double getMonthlyPrice()
    {
        return (double) monthlyPriceCents / 100;
    }

    public double getYearlyPrice()
    {
        return (double) yearlyPriceCents / 100;
    }
}