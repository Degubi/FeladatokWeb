open module degubi.web {
    requires java.compiler;
    requires java.instrument;
    requires java.sql;
    requires java.net.http;

    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires spring.webmvc;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
}