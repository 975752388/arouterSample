package com.key.compiler;

import com.google.auto.service.AutoService;
import com.key.annotation.Router;

import com.key.annotation.model.ArouterBean;
import com.key.annotation.model.Type;

import com.key.compiler.constants.Constants;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.key.annotation.Router")
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({Constants.moduleName, Constants.packageNameForApt})
public class RouterProcessor extends AbstractProcessor {
    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的类文件，class文件以及辅助文件
    private Filer filer;

    // 子模块名，如：app/order/personal。需要拼接类名时用到（必传）ARouter$$Group$$order
    private String moduleName;

    // 包名，用于存放APT生成的类文件
    private String packageNameForAPT;

    // 临时map存储，用来存放路由组Group对应的详细Path类对象，生成路由路径类文件时遍历
    // key:组名"app", value:"app"组的路由路径"ARouter$$Path$$app.class"
    private Map<String, List<ArouterBean>> tempPathMap = new HashMap<>();

    // 临时map存储，用来存放路由Group信息，生成路由组类文件时遍历
    // key:组名"app", value:类名"ARouter$$Path$$app.class"
    private Map<String, String> tempGroupMap = new HashMap<>();
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE,"APT init");
        Map<String, String> options = processingEnv.getOptions();
        if (!EmptyUtils.isEmpty(options)){
            moduleName = options.get(Constants.moduleName);
            packageNameForAPT = options.get(Constants.packageNameForApt);

            // 有坑：Diagnostic.Kind.ERROR，异常会自动结束，不像安卓中Log.e
            messager.printMessage(Diagnostic.Kind.NOTE, "moduleName >>> "+moduleName);
            messager.printMessage(
                    Diagnostic.Kind.NOTE,
                    "packageNameForAPT >>> "+packageNameForAPT
            );
        }
        if (EmptyUtils.isEmpty(moduleName) || EmptyUtils.isEmpty(packageNameForAPT)){
            throw new  RuntimeException("apt need moduleName and packageNameForApt");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (!EmptyUtils.isEmpty(annotations)){
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Router.class);
            try {
                parseAnnotation(elements);
                return true;
            }catch (Exception e){
                messager.printMessage(Diagnostic.Kind.NOTE,"error:"+e.toString());

            }

            return true;

        }
        return false;
    }
    private void parseAnnotation(Set<? extends Element> elements) throws IOException {
        TypeElement activityElement = elementUtils.getTypeElement(Constants.activity);
        TypeElement fragmentElement = elementUtils.getTypeElement(Constants.fragment);
        messager.printMessage(Diagnostic.Kind.NOTE,"fragmentElement="+fragmentElement);
        TypeMirror activityMirror = activityElement.asType();

        for (Element element : elements) {
            TypeMirror elementType = element.asType();
            messager.printMessage(Diagnostic.Kind.NOTE,"element info："+elementType.toString());
            messager.printMessage(Diagnostic.Kind.NOTE,"element==null?"+(element==null));
            Router router = element.getAnnotation(Router.class);
            messager.printMessage(Diagnostic.Kind.NOTE,"router="+router);
            ArouterBean routerBean = new  ArouterBean();

            routerBean.setPath(router.path());
            routerBean.setGroup(router.group());
            routerBean.setElement(element);


            if (typeUtils.isSubtype(elementType,activityMirror)){
                routerBean.setType(Type.ACTIVITY);

            }else if (typeUtils.isSubtype(elementType,fragmentElement.asType())){
                routerBean.setType(Type.FRAGMENT);
            } else{
                //扩展用

            }
            messager.printMessage(Diagnostic.Kind.NOTE,"routerbean type="+routerBean.getType());
            setPathMapValue(routerBean);
            messager.printMessage(Diagnostic.Kind.NOTE,"  setPathMapValue(routerBean)");
        }
        //------------------------------重点-----------
        //开始生成java类

        TypeElement groupTypeElement = elementUtils.getTypeElement(Constants.arouterGroup);//生成group的接口
        TypeElement pathTypeElement =  elementUtils.getTypeElement(Constants.arouterPath);//生成path的接口
        messager.printMessage(Diagnostic.Kind.NOTE,"pathLoad:"+pathTypeElement);
        messager.printMessage(Diagnostic.Kind.NOTE,"groupLoad:"+groupTypeElement);

        createPathClassFile(pathTypeElement);
        createGroupClassFile(groupTypeElement,pathTypeElement);

    }

    private void createGroupClassFile(TypeElement groupTypeElement,TypeElement pathTypeElement) throws IOException {
        if (tempGroupMap.isEmpty()||tempPathMap.isEmpty()){
            return;
        }
        messager.printMessage(Diagnostic.Kind.NOTE,"createGroupClassFile start");
        ParameterizedTypeName methodReturn =  ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathTypeElement)))
        );

        MethodSpec.Builder methodSpec =  MethodSpec.methodBuilder(Constants.groupMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(methodReturn);


        methodSpec.addStatement("$T<$T, $T> $N = new $T<>()",
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathTypeElement))),
                "groupMap",
                HashMap.class);

        messager.printMessage(Diagnostic.Kind.NOTE,"createGroupClassFile new HashMap");
        for (Map.Entry<String, String> entry : tempGroupMap.entrySet()) {
            methodSpec.addStatement("$N.put($S,$T.class)",
                    "groupMap",
                    entry.getKey(),
                    ClassName.get(packageNameForAPT,entry.getValue()));
        }
        methodSpec.addStatement("return $N","groupMap");


        String finalClassName = Constants.groupFileName+moduleName ;
        TypeSpec build = TypeSpec.classBuilder(finalClassName)
                .addSuperinterface(ClassName.get(groupTypeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(methodSpec.build())
                .build();
        JavaFile.builder(packageNameForAPT,build).build().writeTo(filer);
        messager.printMessage(Diagnostic.Kind.NOTE,"createGroupClassFile complete");

    }

    private void createPathClassFile(TypeElement pathTypeElement) throws IOException {
        messager.printMessage(Diagnostic.Kind.NOTE,"pathTypeElement ==null"+(pathTypeElement==null));
        if (tempPathMap.isEmpty()){
            return;
        }
        ParameterizedTypeName methodReturn = ParameterizedTypeName.get(
                ClassName.get(Map.class),
        ClassName.get(String.class),
        ClassName.get(ArouterBean.class)
        );

        for (Map.Entry<String, List<ArouterBean>> entry : tempPathMap.entrySet()) {
            MethodSpec.Builder methodSpec =  MethodSpec.methodBuilder(Constants.pathMethodName)
                    .addAnnotation(Override.class)
                     .addModifiers(Modifier.PUBLIC)
                    .returns(methodReturn);

            //Map<String, RouterBean> pathMap = new HashMap<>();
            methodSpec.addStatement("$T<$T,$T> $N = new $T<>()",
                    ClassName.get(Map.class),
            ClassName.get(String.class),
            ClassName.get(ArouterBean.class),
            "pathMap",
                    HashMap.class);
            // 一个分组，如：ARouter$$Path$$app。有很多详细路径信息，如：/app/MainActivity、/app/OtherActivity
            List<ArouterBean> pathList = entry.getValue();
            //ArouterBean.Companion.create(Type.ACTIVITY)
            for (ArouterBean bean : pathList) {
                messager.printMessage(Diagnostic.Kind.NOTE,"bean:"+bean);
                //pathMap.put("/app/TestActivity",ArouterBean.Companion.create(Type.Activity,TestActivity.class,"/app/TestActivity","app"))
                methodSpec.addStatement("$N.put($S,$T.Companion.create($T.$L,$T.class,$S,$S))",
                        "pathMap",//pathMap
                        bean.getPath(),//"/app/TestActivity"
                        ClassName.get(ArouterBean.class),//ArouterBean
                        ClassName.get(Type.class),//Type
                        bean.getType(),//ACTIVITY
                        ClassName.get((TypeElement)bean.getElement()),//TestActivity
                        bean.getPath(),//"/app/TestActivity"
                        bean.getGroup());//"app"
            }
//            pathList.forEach {  bean->
//                    // pathMap.put("/app/MainActivity", RouterBean.Companion.create(
//                    //        Type.ACTIVITY, MainActivity.class, "/app/MainActivity", "app"));
//                    methodSpec.addStatement("\$N.put(\$S,\$T.Companion.create(\$T.\$L,\$T,\$S,\$S",
//                            "pathMap",
//                            bean.path,
//                            ClassName.get(ArouterBean::class.java),
//                bean.type,
//                        bean.clazz,
//                        bean.path,
//                        bean.group)
//            }

            methodSpec.addStatement("return $N","pathMap");
            String finalClassName = Constants.pathFileName+entry.getKey();
            TypeSpec fileClassName = TypeSpec.classBuilder(finalClassName)
                    .addSuperinterface(ClassName.get(pathTypeElement))
                    .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
                    .addMethod(methodSpec.build())
                    .build();
            JavaFile.builder(packageNameForAPT, fileClassName).build().writeTo(filer);
            tempGroupMap.put(entry.getKey(),finalClassName);

        }


    }

    private void setPathMapValue(ArouterBean routerBean ) {
        if (checkRouterPath(routerBean)){
            List<ArouterBean> routerBeans = tempPathMap.get(routerBean.getPath());
            if (EmptyUtils.isEmpty(routerBeans)){
                routerBeans = new ArrayList();
                routerBeans.add(routerBean);
                tempPathMap.put(routerBean.getGroup(),routerBeans);

            }else{
                routerBeans.add(routerBean);
            }
        }
    }

    private boolean checkRouterPath(ArouterBean routerBean) {
        String path = routerBean.getPath();
        String group = routerBean.getGroup();
        if(EmptyUtils.isEmpty(path)){
            throw new IllegalArgumentException("path can not be null");
            //messager.printMessage(Diagnostic.Kind.NOTE,"path is empty");
        }
        if (!path.startsWith("/")){
            return false;
        }
        if (path.lastIndexOf("/")==0){
            return false;
        }
        //程序健壮可自行扩展


        if (EmptyUtils.isEmpty(group)){
            String finalGroup = path.substring(1,path.indexOf("/",1));
            routerBean.setGroup(finalGroup);

        }
        messager.printMessage(Diagnostic.Kind.NOTE, "group:"+routerBean.getGroup());
        if (!routerBean.getGroup() .equals(moduleName)){


            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter annotation value of group must the same as moduleName");
            return false;
        }

        return true;
    }
}
