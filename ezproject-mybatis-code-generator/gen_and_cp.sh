mvn compile exec:java
target_project_path=$(pwd)/../ezproject-web
cp -rf src/main/java/com/ezone/ezproject/dal/ $target_project_path/src/main/java/com/ezone/ezproject/dal/
cp -rf src/main/resources/mybatis/mapper/ $target_project_path/src/main/resources/mybatis/mapper/