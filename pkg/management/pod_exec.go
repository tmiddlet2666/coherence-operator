/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package management

import (
	"context"
	"errors"
	"io"
	"strings"
	"time"

	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
	utilexec "k8s.io/client-go/util/exec"
)

type ExecRequest struct {
	Pod       string
	Container string
	Namespace string
	Command   []string
	Arg       []string
	Timeout   time.Duration
}

// PodExec executes a command in a Pod.
func PodExec(ctx context.Context, req *ExecRequest, config *rest.Config) (int, string, string, error) {
	kubeClient := kubernetes.NewForConfigOrDie(config)

	timeout := req.Timeout
	if timeout < time.Second*10 {
		timeout = time.Second * 10
	}

	execRequest := kubeClient.CoreV1().RESTClient().Post().
		Timeout(timeout).
		Resource("pods").
		Name(req.Pod).
		Namespace(req.Namespace).
		SubResource("exec").
		Param("stdin", "true").
		Param("stdout", "true").
		Param("stderr", "true").
		Param("tty", "false")

	if req.Container != "" {
		execRequest.Param("container", req.Container)
	}

	for _, cmd := range req.Command {
		execRequest.Param("command", cmd)
	}

	exec, err := remotecommand.NewSPDYExecutor(config, "POST", execRequest.URL())
	if err != nil {
		return 1, "", "", err
	}

	stdIn := newStringReader(req.Arg)
	stdOut := new(streamCapture)
	stdErr := new(streamCapture)

	err = exec.StreamWithContext(ctx, remotecommand.StreamOptions{
		Stdin:  stdIn,
		Stdout: stdOut,
		Stderr: stdErr,
		Tty:    false,
	})

	outStr := strings.Join(stdOut.Str, "")
	errStr := strings.Join(stdErr.Str, "")

	var exitCode int

	if err == nil {
		exitCode = 0
	} else {
		var exitErr utilexec.ExitError
		if errors.As(err, &exitErr) && exitErr.Exited() {
			exitCode = exitErr.ExitStatus()
		}
	}

	return exitCode, outStr, errStr, nil
}

type streamCapture struct {
	Str []string
}

func (w *streamCapture) Write(p []byte) (n int, err error) {
	str := string(p)
	if len(str) > 0 {
		w.Str = append(w.Str, str)
	}
	return len(str), nil
}

func newStringReader(ss []string) io.Reader {
	formattedString := strings.Join(ss, "\n")
	reader := strings.NewReader(formattedString)
	return reader
}
