import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { TextField } from '../../shared/forms/TextField'
import { login } from './api'
import styles from './AuthPage.module.css'
import { returnPath } from './returnTo'
import { loginSchema, type LoginValues } from './schemas'
import { useAuth } from './useAuth'

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [failure, setFailure] = useState<unknown>(null)
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { username: '', password: '' } })
  const { errors, isSubmitting } = form.formState

  const onSubmit = form.handleSubmit(async ({ username, password }) => {
    setFailure(null)
    try {
      signIn(await login(username, password))
      navigate(returnPath(location.state), { replace: true })
    } catch (error) {
      setFailure(error)
    }
  })

  return (
    <section className={styles.page} aria-labelledby="login-title">
      <h1 id="login-title">Sign in</h1>
      <p className={styles.lead}>Signed-in users can add Pokémon to the collection and edit them.</p>
      {failure !== null && <ProblemMessage error={failure} />}
      <form className={styles.form} onSubmit={onSubmit} noValidate>
        <TextField label="Username" autoComplete="username" error={errors.username?.message} {...form.register('username')} />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...form.register('password')}
        />
        <button type="submit" className={styles.submit} disabled={isSubmitting}>
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className={styles.alternative}>
        New here?{' '}
        <Link to="/register" state={location.state}>
          Create an account
        </Link>
      </p>
    </section>
  )
}
