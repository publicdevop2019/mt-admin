cd ../../mt-integration-test
rm -rf ../logs/analytics
mkdir ../logs/analytics
# mvn test -Dtest="PendingUserPerformanceTest" > ../logs/test-run.log
# mvn test -Dtest="UserRegisterPerformanceTest" > ../logs/test-run.log
# mvn test -Dtest="SameUserLoginPerformanceTest" > ../logs/test-run.log
mvn test -Dtest="NewProjectPerformanceTest" > ../logs/test-run.log