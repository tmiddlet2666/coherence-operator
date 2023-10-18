/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package v1_test

import (
	. "github.com/onsi/gomega"
	coh "github.com/oracle/coherence-operator/api/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

func TestHash(t *testing.T) {
	g := NewGomegaWithT(t)

	spec := coh.CoherenceStatefulSetResourceSpec{}

	deployment := &coh.Coherence{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "test-ns",
			Name:      "test",
		},
		Spec: spec,
	}

	coh.EnsureHashLabel(deployment)

	g.Expect(deployment.GetLabels()["coherence-hash"]).To(Equal("5cb9fd9f96"))
}
