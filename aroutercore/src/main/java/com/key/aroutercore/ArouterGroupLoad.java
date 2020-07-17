package com.key.aroutercore;

import java.util.Map;

public interface ArouterGroupLoad {
    Map<String,Class<? extends ArouterPathLoad>> loadGroup();

}
