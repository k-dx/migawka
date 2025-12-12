package main

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"sync"
	"time"

	"github.com/h2non/bimg"
	//jis "github.com/dsoprea/go-jpeg-image-structure/v2"
)

func single() {
	// Prepare command
	cmd := exec.Command("echo", "hello") // replace with your command and args

	// Optional: attach IO if you want visible output or interaction
	// cmd.Stdout = os.Stdout
	// cmd.Stderr = os.Stderr
	// cmd.Stdin = os.Stdin

	// Start the subprocess (returns quickly)
	if err := cmd.Start(); err != nil {
		log.Fatalf("failed to start: %v", err)
	}
	// You can access PID if needed
	log.Printf("started pid=%d", cmd.Process.Pid)

	// Do other work while subprocess runs
	// (example: simulate work with sleep; replace with real work)
	time.Sleep(2 * time.Second)
	log.Println("did some work while subprocess ran")

	// Now wait for the subprocess to finish
	if err := cmd.Wait(); err != nil {
		// If process exited with non-zero status, err is *exec.ExitError
		log.Fatalf("process finished with error: %v", err)
	}

	log.Println("subprocess finished successfully")
}

func multiple() {
	cmds := [][]string{
		{"sleep", "2"},
		{"sleep", "3"},
		{"sleep", "1"},
	}

	var wg sync.WaitGroup
	type result struct {
		idx  int
		err  error
		exit int // -1 if unknown
	}
	results := make(chan result, len(cmds))

	for i, args := range cmds {
		i, args := i, args // capture
		wg.Add(1)
		go func() {
			defer wg.Done()
			cmd := exec.Command(args[0], args[1:]...)

			// Optional: connect IO
			cmd.Stdout = os.Stdout
			cmd.Stderr = os.Stderr

			if err := cmd.Start(); err != nil {
				results <- result{i, err, -1}
				return
			}

			// Do per-process concurrent work here if needed

			err := cmd.Wait()
			exit := -1
			if err == nil {
				if ps := cmd.ProcessState; ps != nil {
					exit = ps.ExitCode()
				}
			}
			results <- result{i, err, exit}
		}()
	}

	// Do other work while subprocesses run
	fmt.Println("main doing other work...")

	// Wait for all goroutines that wrap subprocesses to finish
	wg.Wait()
	close(results)

	// Collect results
	for r := range results {
		if r.err != nil {
			log.Printf("cmd %d finished with error: %v (exit=%d)", r.idx, r.err, r.exit)
		} else {
			log.Printf("cmd %d finished successfully (exit=%d)", r.idx, r.exit)
		}
	}
}

func goroutines() {
	for i := 0; i < 5; i++ {
		go func(i int) {
			log.Printf("goroutine %d starting", i)
			// time.Sleep(time.Duration(i+1) * time.Second)
			log.Printf("goroutine %d done", i)
		}(i)
	}
	time.Sleep(time.Duration(5) * time.Second)
}

func bimg_test() {
	options := bimg.Options{
		Width:         0,
		Height:        400,
		Quality:       80,
		Interlace:     true,
		StripMetadata: false,
		// Compression:  9,
	}

	// Read the file manually
	filePath := "/home/kuba/migawka_media2/P1100282.JPG"
	fileBytes, err := os.ReadFile(filePath)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return
	}

	newImage, err := bimg.NewImage(fileBytes).Process(options)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return
	}

	if err := bimg.Write("new.jpg", newImage); err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
}

type Hash interface {
	FromString(s string) error
	String() string
}

type hashImpl struct {
	data int
}

type xxHash struct {
	data string
}

func NewxxHashFromString(s string) (*xxHash, error) {
	var h xxHash
	err := h.FromString(s)
	if err != nil {
		return nil, err
	}
	return &h, nil
}

func (h *xxHash) FromString(s string) error {
	h.data = s
	return nil
}

func (h *xxHash) String() string {
	return h.data
}

func NewHashFromString(s string) (*hashImpl, error) {
	var h hashImpl
	err := h.FromString(s)
	if err != nil {
		return nil, err
	}
	return &h, nil
}

func (h *hashImpl) FromString(s string) error {
	val, err := strconv.Atoi(s)
	if err != nil {
		return err
	}
	h.data = val
	return nil
}

func (h *hashImpl) String() string {
	return strconv.Itoa(h.data)
}

func do_stuff(m *map[Hash]int, h Hash, val int) {
	(*m)[h] = val
}
func map_interface() {
	m := make(map[Hash]int)

	// one, _ := NewHashFromString("1")
	// m[one] = 42

	// two, _ := NewHashFromString("2")
	// m[two] = 84

	do_stuff(&m, &hashImpl{data: 1}, 42)
	do_stuff(&m, &xxHash{data: "2"}, 84)

	// fmt.Println(m[one])
	// fmt.Println(m[two])
	for k, v := range m {
		fmt.Printf("key: %s, value: %d\n", k.String(), v)
	}
}

func paths() {
	baseDir := "/home/kuba/migawka_media2"
	baseDir2 := "/home/kuba/migawka_media2/"
	baseDir3 := "/home/kuba/../kuba/migawka_media2/"
	relPath := "2023/05/01/P1100282.JPG"

	fullPath := fmt.Sprintf("%s/%s", baseDir, relPath)
	fmt.Println("Full path:", fullPath)

	fmt.Println(filepath.Rel(baseDir, fullPath))
	fmt.Println(filepath.Rel(baseDir2, fullPath))
	fmt.Println(filepath.Rel(baseDir3, fullPath))
}

func main() {
	// multiple()
	// goroutines()
	// bimg_test()
	// map_interface()
	paths()

}
