package v1_test

import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	coherence "github.com/oracle/coherence-operator/pkg/apis/coherence/v1"
)

var _ = Describe("Testing PortSpec struct", func() {

	Context("Copying a PortSpec using DeepCopyWithDefaults", func() {
		var original *coherence.PortSpec
		var defaults *coherence.PortSpec
		var clone *coherence.PortSpec
		var expected *coherence.PortSpec

		NewPortSpecOne := func() *coherence.PortSpec {
			return &coherence.PortSpec{
				Port: int32Ptr(8080),
				SSL:  &coherence.SSLSpec {
					Enabled:                boolPtr(true),
					Secrets:                stringPtr("ssl-secret"),
					KeyStore:               stringPtr("keystore.jks"),
					KeyStorePasswordFile:   stringPtr("storepassword.txt"),
					KeyPasswordFile:        stringPtr("keypassword.txt"),
					KeyStoreAlgorithm:      stringPtr("SunX509"),
					KeyStoreProvider:       stringPtr("fooJCA"),
					KeyStoreType:           stringPtr("JKS"),
					TrustStore:             stringPtr("truststore-guardians.jks"),
					TrustStorePasswordFile: stringPtr("trustpassword.txt"),
					TrustStoreAlgorithm:    stringPtr("SunX509"),
					TrustStoreProvider:     stringPtr("fooJCA"),
					TrustStoreType:         stringPtr("JKS"),
					RequireClientCert:      boolPtr(true),
				},
			}
		}

		NewPortSpecTwo := func() *coherence.PortSpec {
			return &coherence.PortSpec {
				Port: int32Ptr(9090),
				SSL:  &coherence.SSLSpec {
					Enabled:                boolPtr(true),
					Secrets:                stringPtr("ssl-secret2"),
					KeyStore:               stringPtr("keystore.jks"),
					KeyStorePasswordFile:   stringPtr("storepassword2.txt"),
					KeyPasswordFile:        stringPtr("keypassword2.txt"),
					KeyStoreAlgorithm:      stringPtr("SunX509"),
					KeyStoreProvider:       stringPtr("barJCA"),
					KeyStoreType:           stringPtr("JKS"),
					TrustStore:             stringPtr("truststore-guardians2.jks"),
					TrustStorePasswordFile: stringPtr("trustpassword2.txt"),
					TrustStoreAlgorithm:    stringPtr("SunX509"),
					TrustStoreProvider:     stringPtr("barJCA"),
					TrustStoreType:         stringPtr("JKS"),
					RequireClientCert:      boolPtr(false),
				},
			}
		}

		ValidateResult := func() {
			It("should have correct Port", func() {
				Expect(*clone.Port).To(Equal(*expected.Port))
			})

			It("should have correct SSL", func() {
				Expect(*clone.SSL).To(Equal(*expected.SSL))
			})
		}

		JustBeforeEach(func() {
			clone = original.DeepCopyWithDefaults(defaults)
		})

		When("original and defaults are nil", func() {
			BeforeEach(func() {
				original = nil
				defaults = nil
			})

			It("the copy should be nil", func() {
				Expect(clone).Should(BeNil())
			})
		})

		When("defaults is nil", func() {
			BeforeEach(func() {
				original = NewPortSpecOne()
				defaults = nil
				expected = original
			})

			ValidateResult()
		})

		When("original is nil", func() {
			BeforeEach(func() {
				defaults = NewPortSpecOne()
				original = nil
				expected = defaults
			})

			ValidateResult()
		})

		When("all original fields are set", func() {
			BeforeEach(func() {
				original = NewPortSpecOne()
				defaults = NewPortSpecTwo()
				expected = original
			})

			ValidateResult()
		})

		When("original Port is nil", func() {
			BeforeEach(func() {
				original = NewPortSpecOne()
				original.Port = nil
				defaults = NewPortSpecTwo()

				expected = NewPortSpecOne()
				expected.Port = defaults.Port
			})

			ValidateResult()
		})

		When("original SSL is nil", func() {
			BeforeEach(func() {
				original = NewPortSpecOne()
				original.SSL = nil
				defaults = NewPortSpecTwo()

				expected = NewPortSpecOne()
				expected.SSL = defaults.SSL
			})

			ValidateResult()
		})
	})
})