package com.ruskonert.GamblKing.engine.program;

import com.ruskonert.GamblKing.engine.program.component.PreloadComponent;
import com.ruskonert.GamblKing.engine.program.component.ProgramComponent;

public class ProgramManager
{
    private static PreloadComponent preloadComponent;
    public static PreloadComponent getPreloadComponent() { return preloadComponent; }

    private static ProgramComponent programComponent;
    public static ProgramComponent getProgramComponent() { return programComponent; }
}
