import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { applyServerFieldErrors } from '../../shared/forms/serverErrors'
import { TextField } from '../../shared/forms/TextField'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { login, register } from './api'
import styles from './AuthPage.module.css'
import { returnPath } from './returnTo'
import { registerSchema, type RegisterValues } from './schemas'
import { useAuth } from './useAuth'

const LABELS = { username: 'Username', email: 'Email', password: 'Password' } as const

export function RegisterPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [failure, setFailure] = useState<unknown>(null)
  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: '', email: '', password: '' },
  })
  const { errors, isSubmitting } = form.formState

  const onSubmit = form.handleSubmit(async ({ username, email, password }) => {
    setFailure(null)
    try {
      await register(username, email, password)
      signIn(await login(username, password))
      navigate(returnPath(location.state), { replace: true })
    } catch (error) {
      if (!applyServerFieldErrors(error, form.setError, LABELS)) {
        setFailure(error)
      }
    }
  })

  return (
    <section className={styles.page} aria-labelledby="register-title">
      <h1 id="register-title">Create an account</h1>
      <p className={styles.lead}>New accounts can add Pokémon to the collection and edit them.</p>
      {failure !== null && <ProblemMessage error={failure} />}
      <form className={styles.form} onSubmit={onSubmit} noValidate>
        <TextField label="Username" autoComplete="username" error={errors.username?.message} {...form.register('username')} />
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...form.register('email')} />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="At least 8 characters"
          error={errors.password?.message}
          {...form.register('password')}
        />
        <button type="submit" className={styles.submit} disabled={isSubmitting}>
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className={styles.alternative}>
        Already registered?{' '}
        <Link to="/login" state={location.state}>
          Sign in
        </Link>
      </p>
    </section>
  )
}
