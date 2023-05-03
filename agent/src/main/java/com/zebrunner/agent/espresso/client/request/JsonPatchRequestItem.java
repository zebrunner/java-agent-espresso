package com.zebrunner.agent.espresso.client.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JsonPatchRequestItem {

    private String op;
    private String path;
    private String value;

}
