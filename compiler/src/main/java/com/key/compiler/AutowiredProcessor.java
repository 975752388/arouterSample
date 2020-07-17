package com.key.compiler;

import com.google.auto.service.AutoService;
import com.key.annotation.Autowired;
import com.key.compiler.constants.Constants;

import java.io.IOException;
import java.io.Writer;
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
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
@SupportedAnnotationTypes("com.key.annotation.Autowired")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutowiredProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Map<TypeElement, List<Element>> tempMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        messager.printMessage(Diagnostic.Kind.NOTE,"AutowiredProcessor init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!EmptyUtils.isEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Autowired.class);
            try {
                messager.printMessage(Diagnostic.Kind.NOTE,"AutowiredProcessor parseAnnotation before");
                parseAnnotation(elements);
                messager.printMessage(Diagnostic.Kind.NOTE,"AutowiredProcessor parseAnnotation after:"+tempMap);
                createFile();
                messager.printMessage(Diagnostic.Kind.NOTE,"AutowiredProcessor createFile after");
            }catch (Exception e){
                messager.printMessage(Diagnostic.Kind.NOTE,"error:"+e);
            }
            return true;

        }
        return false;
    }
    private void createFile() throws IOException {
        TypeElement activity = elementUtils.getTypeElement(Constants.activity);
        for (Map.Entry<TypeElement, List<Element>> entry : tempMap.entrySet()) {
            TypeElement element = entry.getKey();
            if (!typeUtils.isSubtype(element.asType(),activity.asType())){
                continue;
            }
            JavaFileObject javaFileObject = filer.createSourceFile(element.getSimpleName().toString()+Constants.autowiredSufix);
            Writer writer = javaFileObject.openWriter();
            writer.write("package "+elementUtils.getPackageOf(element).getQualifiedName().toString()+";\n");
            messager.printMessage(Diagnostic.Kind.NOTE,"packageName:"+"package"+elementUtils.getPackageOf(element).getQualifiedName().toString());
            writer.write("import com.key.aroutercore.IAutowired;\n");
            String targetActivity = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE,"target:"+targetActivity);
            writer.write("public final class "+targetActivity+Constants.autowiredSufix+" implements IAutowired{\n");
            writer.write("    public void loadParameter(Object target) {\n");
            writer.write("        "+targetActivity+" activity = ("+targetActivity+")target;\n");

            for (Element fieldElement : entry.getValue()) {
                Autowired autowired = fieldElement.getAnnotation(Autowired.class);
                String name = autowired.name();
                String filedName = fieldElement.getSimpleName().toString();
                if (EmptyUtils.isEmpty(name)){
                    name = filedName;
                }
                messager.printMessage(Diagnostic.Kind.NOTE,"filedName:"+name);
                int type = fieldElement.asType().getKind().ordinal();
                String getExtra="getStringExtra";
                if (type == TypeKind.INT.ordinal()){
                    getExtra = "getIntExtra";
                }else if (type == TypeKind.BOOLEAN.ordinal()){
                    getExtra = "getBooleanExtra";
                }else if (type == TypeKind.LONG.ordinal()){
                    getExtra="getLongExtra";
                }else {
                    if (fieldElement.asType().toString().equalsIgnoreCase("java.lang.String")){
                        getExtra = "getStringExtra";
                    }
                }
                if ("getStringExtra".equals(getExtra)){
                    writer.write("        activity."+filedName+" = activity.getIntent()."+getExtra+"("+"\""+name+"\""+");\n");
                }else
                    writer.write("        activity."+filedName+" = activity.getIntent()."+getExtra+"("+"\""+name+"\""+",activity."+filedName+");\n");
            }

            writer.write(" }\n");
            writer.write("}");
            writer.flush();
            writer.close();
        }
    }

    private void parseAnnotation(Set<? extends Element> elements){

        if (!EmptyUtils.isEmpty(elements)){
            for (Element element : elements) {
                Autowired autowired = element.getAnnotation(Autowired.class);

                TypeElement enclosingElement =(TypeElement) element.getEnclosingElement();


                if (tempMap.containsKey(enclosingElement)){
                    tempMap.get(enclosingElement).add(element);
                }else {
                    List<Element> fields = new ArrayList<>();
                    fields.add(element);
                    tempMap.put(enclosingElement,fields);
                }

            }
        }

    }
}
