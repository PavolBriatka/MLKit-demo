pipeline {
    agent { label 'windows' }
    stages {
        stage('build') {
            steps {
                echo 'Cleaning'
                bat "gradlew clean"
            }
        }
    }
}