package com.key.aroutercore;

import com.key.annotation.model.ArouterBean;

import java.util.Map;

public interface ArouterPathLoad {
    Map<String, ArouterBean> loadPath();
}
