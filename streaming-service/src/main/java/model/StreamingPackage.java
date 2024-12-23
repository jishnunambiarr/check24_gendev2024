package model;

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

    private double getMonthlyPrice()
    {
        return monthlyPriceCents / 100;
    }

    private double getYearlyPrice()
    {
        return yearlyPriceCents / 100;
    }
}