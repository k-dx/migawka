package main

import (
	"os"
	"os/exec"
	"testing"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
)

func TestMain(m *testing.M) {
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout})
	zerolog.SetGlobalLevel(zerolog.WarnLevel)

	exitCode := m.Run()
	os.Exit(exitCode)
}

func copyDir(t *testing.T, src string, dst string) {
	cmd := exec.Command("cp", "-r", src, dst)
	out, err := cmd.CombinedOutput()
	if err != nil {
		// handle error; out contains combined stdout/stderr
		t.Fatalf("Failed to copy directory from %s to %s: %v, output: %s", src, dst, err, string(out))
	}
}
