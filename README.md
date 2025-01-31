# UncheckedException  

A tool to identify and compare unchecked exceptions in Java applications and libraries.  
![image](https://github.com/user-attachments/assets/94236f3e-c39c-45b4-a911-7f331934eed3)

---

## 📋 Overview  
UncheckedException is a powerful utility to:  
1. Detect all unchecked exceptions present in a Java application.  
2. Compare versions of a Java library to find newly added unchecked exceptions.  

---

## 🚀 Getting Started  

### Using the Java Application  
To find unchecked exceptions in the code, follow these steps:  

1. **Build the Project**  
   ```bash  
   mvn clean package  
   ```  

2. **Run the Application**  
   ```bash  
   java -cp target/unexpectedException-1.0-SNAPSHOT.jar org.vinayak.Main <pathToJarFile> <libraryName>  
   ```  

   This will generate a JSON file named `<libraryName>.json` containing the unchecked exceptions found.  

---

### Using the Python Script  
To compare unchecked exceptions between two versions of a library:  

1. **Run the Python Script**  
   ```bash  
   python3 transitiveException.py libraryNameOld libraryNameNew  
   ```  

2. **Prepare the Resources**  
   Ensure the JAR files for the libraries you are comparing are located in the `resources` folder.  
   For example, to compare `asm-5.1` with `asm-7.2`:  
   - Place `asm-5.1.jar` and `asm-7.2.jar` in the `resources` folder.  
   - Run the script:  
     ```bash  
     python3 transitiveException.py asm-5.1 asm-7.2  
     ```  

   The output will be a JSON file named `asm-5.1->asm-7.2.json`.  

---

## 📂 Output Format  
The output JSON files provide a structured summary of unchecked exceptions, helping you track changes and identify newly introduced issues.
Following is the format of the JSON file created by the JAVA application (individual library report):
```json
[
    {
        "ClassName": [
            {
                "methodSignature": "Method signature including return type, method name, and parameter types.",
                "external_method_calls": ["List of external methods called by this method."],
                "internal_method_calls": ["List of internal methods called within the same library."],
                "unchecked_exceptions": ["List of unchecked exceptions that this method may throw."]
            },
            ...
        ]
    },
    ...
]
```

---

## 🛠 Prerequisites  
- **Java**: Ensure you have Maven installed for building the Java project.  
- **Python**: Make sure Python 3.x is installed for running the comparison script.  

---

## 💡 Example  
To analyze a library named `myLibrary.jar` and compare two versions:  

1.  **Place the JAR files in resources**
2.  **Build Java Program**
   ```bash  
   mvn clean package  
   ```  

3. **Compare Versions**  
   ```bash  
   python3 transitiveException.py myLibrary-v1 myLibrary-v2  
   ```  

The resulting `myLibrary-v1->myLibrary-v2.json` will highlight differences.  
