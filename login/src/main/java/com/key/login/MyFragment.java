package com.key.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.key.annotation.Router;

@Router(path = "/login/MyFragment")
public class MyFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_login,container,false);
        TextView textView = view.findViewById(R.id.textView);
        textView.setText("my fragment");
        return view;
    }
    private MyFragment(){}
}
