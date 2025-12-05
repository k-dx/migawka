package main

import "testing"

func IsPathInsideBaseTestCase(t *testing.T, basePath, targetPath string, expectedResult bool) {
	ok, err := IsPathInsideBase(basePath, targetPath)
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if ok != expectedResult {
		t.Fatalf("Expected result %v, got %v for basePath='%s', targetPath='%s'", expectedResult, ok, basePath, targetPath)
	}
}

func Test_isPathInsideBase(t *testing.T) {
	IsPathInsideBaseTestCase(t, "/media", "/media/photos/img1.jpg", true)
	IsPathInsideBaseTestCase(t, "./media", "./media/photos/img1.jpg", true)
	IsPathInsideBaseTestCase(t, "/media", "/media/..", false)
	IsPathInsideBaseTestCase(t, "/media", "/media/.", true)
	IsPathInsideBaseTestCase(t, "/media", "/media/dir/../../", false)
}
