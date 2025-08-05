# UncheckedExceptionChangeDetector

A tool to identify and compare unchecked exceptions thrown in Java applications and libraries.
![image](https://github.com/user-attachments/assets/94236f3e-c39c-45b4-a911-7f331934eed3)

---

## 📋 Overview  
UncheckedExceptionChangeDetector can:
1. Create a list of all unchecked exceptions explicitly thrown in a Java application.  
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
   python3 findUCBBC.py <owner/repo> <sha>  
   ```  

   The output will be several JSON files:
   - individual library analysis results saved in ./LibraryResults
   - comparison result of libraries saved in ./CompareResults
   - Library-Client match result saved in ./Match
   - It will also update or create the result.csv and combined_results.csv in ./results

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
   cd scripts
   python3 findUCBCC.py <owner/repo> <sha>  
   ```  
